import React from 'react'
import styles from './Account.module.css'
import TextField from '@mui/material/TextField';
import axios from 'axios';
import { BASE_URL } from '../../constants';

function AccountDetail() {
  const [accountNumber, setAccountNumber] = React.useState<string>('');
  const [identificationNumber, setIdentificationNumber] = React.useState<string>('');
  
  
  const AccountNumberChange = (e) => {
    setAccountNumber(e.target.value)
  }
  const IdentificationNumberChange = (e) => {
    setIdentificationNumber(e.target.value)
  }

  const data = {
    "identificationNumber": identificationNumber
  }

  const SubmitCreate = async(e) => {
    e.preventDefault()
    try {
      const response = await axios.post(`${BASE_URL}/bank/owner/create`, data)
      console.log('complete! :', response)
    } catch {
      console.log('fail')
    }
  }
  
    return (
      <div className={styles.container}>
        <div className={styles.content}>
          
          <div className={styles.contentbanner}>
            <div className={styles.title}>계좌 상세 조회</div>
            <div className={styles.information}>계좌를 상세히 조회해보세요</div>
          </div>

          <div className={styles.contentbox}>
            <form onSubmit={SubmitCreate} className={styles.inputform}>
              <TextField className={styles.inputbox} id="outlined-basic" label="식별번호" variant="outlined" onChange={IdentificationNumberChange} style={{marginTop : "10px"}}/><br />
              <TextField className={styles.inputbox} id="outlined-basic" label="계좌번호" variant="outlined" onChange={AccountNumberChange} style={{marginTop : "10px"}}/>
              <button className={styles.submitbutton} onClick={SubmitCreate}>계좌 생성</button>
            </form>
          </div>

        </div>
      </div>
    )
}

export default AccountDetail